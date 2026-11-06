# Repository setup and publishing

Target repository: https://github.com/Grimmoir-Organization/Lunera

## GitHub About

Description: Local Android music player with folder browsing, synchronized lyrics, a sleep timer and customizable player layouts. A Grimmoir Organization project.

Topics: lunera, android, kotlin, music-player, local-music, offline-music, lyrics, sleep-timer, apk, gplv3, grimmoir-organization

Website: https://github.com/Grimmoir-Organization/Lunera/releases

## Upload source

Extract Lunera-public-repo.zip and put the CONTENTS of its Lunera folder at the repository root. Keep README.md and assets/ together. The supplied files include the complete regular-app source and required modules; APKs belong in release assets.

If reusing a private repository, review its existing tracked files and history for signing keys or credentials before changing visibility. The supplied clean ZIP contains no signing keys or credentials. Preserve the private keystore and local.properties in your local checkout; do not commit them.

The supplied .gitignore excludes old downloads/report exports. Existing tracked artifacts must also be removed from Git's index if they are already in the repository; copying a clean ZIP alone does not remove old tracked files. Keep docs/ROLLBACK.md and this docs/PUBLISHING.md.

## Create the 1.8.3 release

1. Commit and push this source snapshot to the chosen branch.
2. Create and push tag v1.8.3 against that commit. If the tag already exists, inspect it first; do not force-move a published tag.
3. On GitHub choose Releases > Draft a new release and select v1.8.3.
4. Title: Lunera 1.8.3. Paste RELEASE_NOTES.md from the release-files bundle into the description.
5. Attach Lunera-1.8.3.apk, Lunera-1.8.3-source.zip, changelog.json and SHA256SUMS.txt from that bundle.
6. Review the known Android 15 X-button crash and downgrade limitation. This is a rollback baseline, not a newly validated stable release.
7. Save as draft while reviewing. Publish when ready and choose its latest/pre-release status deliberately.

GitHub's automatic Source code archives now contain the actual project because this is the application's public source repository.

The optional PowerShell script in the release bundle creates a DRAFT release only and requires an existing remote tag plus an authenticated GitHub CLI. It never publishes automatically.

## Validation scope

This packaging operation preserves original 1.8.3 runtime source/resources and its original signed APK. It does not rebuild or retest the app. Historical 1.8.3 build results apply to that original build. Runtime-file identity and APK checksums are verified in this package.
