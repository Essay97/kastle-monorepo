# Kastle releases

## Version policy

`gradle.properties` is the source of truth for the repository version. Root, API,
CLI and sample build artifacts inherit it. `./gradlew -q appVersion` prints it.
The current development version is **0.1.2-SNAPSHOT**; this is not a published release.

Use `MAJOR.MINOR.PATCH-SNAPSHOT` during development, and `MAJOR.MINOR.PATCH` for
releases tagged `vMAJOR.MINOR.PATCH`. Increment patch for compatible fixes and
dependency updates, minor for features, and major for breaking changes. During
0.x development, use a minor increment for breaking API changes and document them.
Never move a released tag or replace a published artifact.

A release may publish only the CLI. For example, a CLI dependency update can ship
CLI 0.1.3 while the latest published API remains 0.1.2. No empty API release is
required. The CLI bundles the API built from that release's source commit. If API
code or its runtime dependencies change, publish both API and CLI from the same
tag. API publication versions may therefore skip numbers. Game metadata versions
remain independent.

## Prepare without publishing

Requires Git, Python 3 for release-check tests, a JDK capable of running Gradle 8.10 (17 recommended), and an
available JDK 11 toolchain for compilation. Use the checked-in Gradle wrapper.

```sh
./gradlew clean test build
python3 -B -m unittest discover -s scripts -p 'test_*.py'
./gradlew :api:deployLocal :api:generatePomFileForCentralPortalJavaComponentPublication
```

Local publication writes to `build/maven-local`, needs no credentials and does
not contact Maven Central for publication. Normal dependency downloads may occur.
The CLI ZIP is `engine/build/distributions/kastle-<version>.zip`; the API coordinate
is `com.saggiodev:kastle-api:<version>`. The ZIP bundles its runtime dependencies.
Do not use `deployAll` for local verification: it includes remote publication.

## Release when ready

1. Finish the intended fixes, change the single version to a stable value, and
   commit the release changes. Run the verification commands above.
2. Create and push an annotated tag matching the version, e.g. `v0.1.2`.
3. Run the existing Jenkins API and/or engine jobs with `RELEASE_TAG=v0.1.2`.
   For a combined release, publish API first; then run the engine job with the
   same tag. Both jobs check out that tag, verify its version and run all builds
   and tests before publishing. These two publications are not transactional.
   If only one succeeds, retry only the missing publication after investigation.
4. Verify the selected artifacts are available before updating `kastle-docs`:
   installation download URLs use the latest published CLI version; Gradle
   dependency examples use the latest published API version. Record both versions
   and the tag in release notes. Do not advertise snapshots as downloadable releases.
5. Advance the development version to the next intended `-SNAPSHOT` in a new commit.

The equivalent API publishing commands from a clean tagged checkout are:

```sh
sh scripts/check-release.sh v0.1.2
./gradlew --no-daemon clean test build
./gradlew --no-daemon :api:deployCentralPortal
```

The last command publishes and finalizes on Maven Central; it is not a dry run.
Only run it when a release is intended, after the preceding checks pass.
The engine Jenkins job deploys the built ZIP to
`/var/www/kastle-docs/attachments/kastle-<version>.zip`. It refuses to overwrite
an existing archive and exposes the completed file atomically. A failed retry
against an existing file requires checking the already published artifact.
GitHub hosts source tags; this process does not create GitHub Releases or assets.

## Jenkins and credentials

Retain the existing Pipeline-from-SCM jobs pointing to `api/Jenkinsfile` and
`engine/Jenkinsfile`. They now accept `RELEASE_TAG`, replacing `TAG_OR_BRANCH`.
The job's pipeline definition must include this updated flow before its first
release run. Git, Pipeline, Credentials Binding and Workspace Cleanup plugins
are required; the Git Parameter plugin is no longer needed by these files.
SCM repository URL and checkout credentials come from the job's SCM configuration.

On the API job, supply these Jenkins **Secret text** credentials (IDs and bound
environment variable names are identical):

| Credential | Value |
| --- | --- |
| `UPLOAD_USERNAME` | Central Portal user-token username |
| `UPLOAD_PASSWORD` | Central Portal user-token password |
| `SIGNING_KEY` | ASCII-armored private PGP signing key, including real newlines |
| `SIGNING_PASSPHRASE` | Signing key passphrase |

Credentials are bound only during API publication. Local publication operators
can supply the same environment variables through their secret manager. The
[MavenDeployer secret lookup](https://opensource.deepmedia.io/deployer/configuration)
supports environment variables, so Jenkins no longer needs the `local.properties`
file credential. Never commit credentials or echo them in logs.

The engine job must run on the docs server (as before), with noninteractive sudo
permission for the file operations in its Jenkinsfile and an existing attachments
directory. API jobs need outbound access to Central Portal. Both jobs clean their
workspace in `post.always`, including on failure. No live Jenkins or publication
service configuration is changed by editing this repository.
