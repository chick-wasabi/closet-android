#!/usr/bin/env python3
"""Create once, restore securely, and verify one permanent APK signing identity."""
import argparse, base64, hashlib, json, os, pathlib, re, secrets, shutil, subprocess, sys, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
PIN = ROOT / 'config/signing-certificate.sha256'

def private_write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as stream: stream.write(data)
    path.chmod(0o600)

def keytool():
    java = os.environ.get('JAVA_HOME')
    return str(pathlib.Path(java) / 'bin/keytool') if java else (shutil.which('keytool') or 'keytool')

def certificate(store, password, alias):
    env = dict(os.environ, CLOSET_KEY_PASSWORD=password)
    result = subprocess.run([keytool(), '-exportcert', '-keystore', str(store), '-storepass:env', 'CLOSET_KEY_PASSWORD', '-alias', alias], env=env, capture_output=True)
    if result.returncode: raise ValueError('Cannot read signing certificate. Check the configured signing bundle.')
    return result.stdout

def verify_config(config):
    config_path = pathlib.Path(config).resolve()
    if config_path.is_relative_to(ROOT): raise ValueError('Keep signing configuration outside the repository.')
    data = json.loads(config_path.read_text())
    if pathlib.Path(data['store_file']).resolve().is_relative_to(ROOT): raise ValueError('Keep the private keystore outside the repository.')
    actual = hashlib.sha256(certificate(data['store_file'], data['password'], data['alias'])).hexdigest()
    if actual != PIN.read_text().strip(): raise ValueError('Signing certificate mismatch. Refusing to replace the permanent identity.')
    return data

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    init = sub.add_parser('init'); init.add_argument('--output', required=True)
    restore = sub.add_parser('restore'); restore.add_argument('--bundle-file'); restore.add_argument('--output-dir', required=True)
    sub.add_parser('verify')
    apks = sub.add_parser('verify-apks'); apks.add_argument('apks', nargs='+')
    args = parser.parse_args()
    if args.command == 'init':
        output = pathlib.Path(args.output).resolve()
        if output.is_relative_to(ROOT): raise ValueError('Keep signing credentials outside the repository.')
        if PIN.exists() or output.exists(): raise ValueError('Signing identity already exists. Restore it; never generate a replacement.')
        output.parent.mkdir(parents=True, exist_ok=True, mode=0o700); output.parent.chmod(0o700)
        password = secrets.token_urlsafe(36)
        alias = 'closet'
        with tempfile.TemporaryDirectory(dir=output.parent) as folder:
            store = pathlib.Path(folder) / 'closet.p12'
            env = dict(os.environ, CLOSET_KEY_PASSWORD=password)
            result = subprocess.run([keytool(), '-genkeypair', '-storetype', 'PKCS12', '-keystore', str(store), '-alias', alias, '-keyalg', 'RSA', '-keysize', '3072', '-validity', '36500', '-dname', 'CN=Closet, O=Closet', '-storepass:env', 'CLOSET_KEY_PASSWORD', '-keypass:env', 'CLOSET_KEY_PASSWORD'], env=env, capture_output=True)
            if result.returncode: raise ValueError('Signing key generation failed.')
            der = certificate(store, password, alias)
            fingerprint = hashlib.sha256(der).hexdigest()
            private_write(output, json.dumps({'version': 1, 'keystore_base64': base64.b64encode(store.read_bytes()).decode(), 'password': password, 'alias': alias}))
            PIN.parent.mkdir(exist_ok=True)
            PIN.write_text(fingerprint + '\n')
        print('Permanent signing identity created. Private bundle is outside the repository; back it up securely.')
    elif args.command == 'restore':
        raw = pathlib.Path(args.bundle_file).read_text() if args.bundle_file else os.environ.get('CLOSET_SIGNING_BUNDLE', '')
        if not raw: raise ValueError('Missing CLOSET_SIGNING_BUNDLE. Add the existing permanent bundle in repository Actions secrets; do not generate a new key.')
        data = json.loads(raw)
        if data.get('version') != 1: raise ValueError('Unsupported signing bundle version.')
        folder = pathlib.Path(args.output_dir).resolve()
        if folder.is_relative_to(ROOT): raise ValueError('Restore signing material outside the repository.')
        folder.mkdir(parents=True, exist_ok=True, mode=0o700); folder.chmod(0o700)
        store = folder / 'closet.p12'
        decoded = base64.b64decode(data['keystore_base64'], validate=True)
        fd = os.open(store, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        with os.fdopen(fd, 'wb') as stream: stream.write(decoded)
        store.chmod(0o600)
        config = folder / 'signing.json'
        private_write(config, json.dumps({'store_file': str(store), 'password': data['password'], 'alias': data['alias']}))
        verify_config(config)
        github_env = os.environ.get('GITHUB_ENV')
        if github_env:
            with open(github_env, 'a') as stream: stream.write('CLOSET_SIGNING_CONFIG=' + str(config) + '\n')
        print('Permanent signing identity restored and certificate verified.')
        print('Signing configuration path: ' + str(config))
    elif args.command == 'verify':
        config = os.environ.get('CLOSET_SIGNING_CONFIG')
        if not config: raise ValueError('CLOSET_SIGNING_CONFIG is required to package any APK. Run scripts/signing.py restore with the permanent bundle first.')
        verify_config(config); print('Signing identity matches the pinned certificate.')
    else:
        sdk = pathlib.Path(os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', '')))
        signer = sdk / 'build-tools/35.0.0/apksigner'
        for apk in args.apks:
            result = subprocess.run([str(signer), 'verify', '--print-certs', apk], capture_output=True, text=True)
            match = re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)', result.stdout)
            if result.returncode or not match or match.group(1) != PIN.read_text().strip(): raise ValueError('APK certificate verification failed: ' + apk)
        print('All APKs use the permanent signing certificate.')

if __name__ == '__main__':
    try: main()
    except (ValueError, KeyError, OSError, json.JSONDecodeError) as error:
        print('Signing setup failed: ' + str(error), file=sys.stderr); sys.exit(1)
