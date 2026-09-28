"""Exercise release checks in disposable Git repositories; no remote publication."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).with_name("check-release.sh").resolve()


class ReleaseCheckTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = pathlib.Path(self.directory.name)
        self.git("init", "-q")
        self.git("config", "user.email", "release-test@example.invalid")
        self.git("config", "user.name", "Release test")
        shutil.copy(SCRIPT, self.root / "check-release.sh")
        wrapper = self.root / "gradlew"
        wrapper.write_text("#!/bin/sh\ncat version\n")
        wrapper.chmod(0o755)
        (self.root / "version").write_text("0.1.2\n")
        self.commit()
        self.git("tag", "-a", "v0.1.2", "-m", "test release")

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.root, check=True,
                              capture_output=True, text=True)

    def commit(self):
        self.git("add", ".")
        self.git("-c", "commit.gpgsign=false", "commit", "-qm", "fixture")

    def check(self, tag, expected=None):
        result = subprocess.run(["sh", "check-release.sh", tag], cwd=self.root,
                                capture_output=True, text=True)
        if expected is None:
            self.assertEqual(result.returncode, 0, result.stderr)
        else:
            self.assertNotEqual(result.returncode, 0)
            self.assertIn(expected, result.stderr)

    def test_matching_annotated_tag(self):
        self.check("v0.1.2")

    def test_invalid_tag(self):
        for tag in ["main", "v0.1.2-SNAPSHOT", "v01.1.2", "v0.1.2; echo bad"]:
            self.check(tag, "stable version tag")

    def test_missing_tag(self):
        self.check("v0.1.3", "tag does not exist")

    def test_dirty_tree(self):
        (self.root / "version").write_text("0.1.3\n")
        self.check("v0.1.2", "working tree must be clean")

    def test_untracked_file(self):
        (self.root / "unexpected").touch()
        self.check("v0.1.2", "working tree must be clean")

    def test_wrong_commit(self):
        (self.root / "new-file").touch()
        self.commit()
        self.check("v0.1.2", "HEAD does not match")

    def test_version_mismatch_and_snapshot(self):
        for version in ["0.1.3", "0.1.2-SNAPSHOT"]:
            (self.root / "version").write_text(version + "\n")
            self.commit()
            self.git("tag", "-f", "v0.1.2")
            self.check("v0.1.2", "does not match project version")
