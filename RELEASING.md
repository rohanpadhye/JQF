These notes are for the project maintainers to help with releasing artifacts to Maven Central.

> **Note (2025):** Sonatype's legacy OSSRH service (`oss.sonatype.org`) reached
> end-of-life on June 30, 2025 and has been shut down. Publishing now goes
> through the [Central Portal](https://central.sonatype.com/). The
> `edu.berkeley.cs.jqf` namespace was automatically migrated there. Releases are
> deployed with the `central-publishing-maven-plugin` (configured in `pom.xml`).

# One-time setup

1. Log in to the [Central Portal](https://central.sonatype.com/) with your
   Sonatype credentials and confirm the `edu.berkeley.cs.jqf` namespace is
   listed and verified.

2. Generate a **User Token** on the Portal (Account → Generate User Token), then
   add it to `~/.m2/settings.xml`. Note the server `id` must be `central` (the
   `publishingServerId` referenced by the plugin), and the username/password are
   the token's values — **not** your Portal login password, and **not** an old
   OSSRH token (those now return `401`):

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>$token_username</username>
      <password>$token_password</password>
    </server>
  </servers>
</settings>
```

3. Configure PGP keys locally using GnuPG (aka GPG) so that artifacts can be
   signed. Central still requires signed `.asc` files alongside the main,
   sources, and javadoc jars. Make sure your public key is published to a key
   server.


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

This uploads a **deployment** (all signed artifacts, bundled) to the Central
Portal. It does *not* publish automatically — the deployment waits for manual
review (the plugin is configured without `autoPublish`).

4. Update `scripts/jqf-driver.sh` with the new version number (the `version=` line).

5. Log on to the [Central Portal Publishing page](https://central.sonatype.com/publishing)
   to review the deployment. Once validation passes, click **Publish** to
   release it to Maven Central. (It may take some time to sync and become
   searchable.)

6. If the release went fine and changes were not pushed in step 2, then run `git push` (and push the tag).
