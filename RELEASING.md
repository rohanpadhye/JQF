These notes are for the project maintainers to help with releasing artifacts to Maven Central.

# One-time setup

1. Set up `~/.m2/settings.xml` with `$username` and `$password` from `https://oss.sonatype.org/`:

```
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>$username</username>
      <password>$password</password>
    </server>
  </servers>
</settings>
```

2. Configure PGP keys locally using GnuPG (aka GPG) so that artifacts can be signed.


# Every Release

1. Make sure GPG can use TTY to get password. In bash:
```bash
export GPG_TTY=$(tty)
```

2. Prepare release:
```
mvn release:prepare -P release-profile -DpushChanges=false
```

The `-DpushChanges=false` avoids pushing the new release commits to GitHub, in case there's some problem that requires a rollback.

3. Check if everything went okay, then deploy:
```
mvn release:perform -P release-profile -DlocalCheckout
```

The `-DlocalCheckout` is needed if changes were not pushed above. This tells Maven to use the new release from the local repo instead of pulling from GitHub.

4. Update `scripts/jqf-driver.sh` and `scripts/instrument.sh` with new version numbers.

5. Log on to the [Nexus Repository Manager](https://oss.sonatype.org) to close + release. Wait.

6. If the release went file and changes were not pushed in step 2, then run `git push`.
