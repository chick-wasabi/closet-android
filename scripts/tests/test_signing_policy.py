import os
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]

class SigningPolicyTest(unittest.TestCase):
    def run_signing(self, *args, **extra):
        env = dict(os.environ)
        env.pop('CLOSET_SIGNING_CONFIG', None)
        env.pop('CLOSET_SIGNING_BUNDLE', None)
        env.update(extra)
        return subprocess.run(['python3', str(ROOT/'scripts/signing.py'), *args], env=env, capture_output=True, text=True)

    def test_missing_identity_is_rejected(self):
        result = self.run_signing('verify')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('required', result.stderr)

    def test_existing_identity_cannot_be_regenerated(self):
        pin = (ROOT/'config/signing-certificate.sha256').read_bytes()
        with tempfile.TemporaryDirectory() as folder:
            output = pathlib.Path(folder)/'bundle.json'
            result = self.run_signing('init', '--output', str(output))
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse(output.exists())
        self.assertEqual(pin, (ROOT/'config/signing-certificate.sha256').read_bytes())

    def test_restoring_inside_repository_is_rejected(self):
        result = self.run_signing('restore', '--output-dir', str(ROOT/'unsafe-signing-output'), CLOSET_SIGNING_BUNDLE='{"version":1}')
        self.assertNotEqual(result.returncode,0)
        self.assertIn('outside the repository',result.stderr)
        self.assertFalse((ROOT/'unsafe-signing-output').exists())

    def test_bad_bundle_does_not_leak_input(self):
        marker = 'example-private-value-not-for-logs'
        with tempfile.TemporaryDirectory() as folder:
            result = self.run_signing('restore','--output-dir',folder,CLOSET_SIGNING_BUNDLE=marker)
            self.assertNotEqual(result.returncode, 0)
            self.assertNotIn(marker,result.stdout+result.stderr)

if __name__=='__main__': unittest.main()
