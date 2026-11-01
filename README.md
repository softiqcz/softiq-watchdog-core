# softiq-watchdog-core

Shared Java library for `softiq-app` and `softiq-watchdog-app`.

Coordinates: `cz.softiq.watchdog:softiq-watchdog-core:1.0.0`. Java 24, Spring Boot dependency management 3.5.3.

Contains shared business services, entity/repository mappings, reporting persistence, watchdog settings/results and email templates. It has no application entry point, HTTP controllers, schedules, browser pages, scrapers, Docker orchestration or executable suites. Consumers explicitly scan `cz.softiq.common`, `cz.softiq.service` and the required portions of `cz.softiq.core`.

```sh
./gradlew check build
./gradlew publishToMavenLocal
# Explicit remote publication, after review and package configuration:
./gradlew publishMavenJavaPublicationToGitHubPackagesRepository
```

Set `GITHUB_USERNAME` and `GITHUB_TOKEN`, or Gradle properties `github.username` and `github.token`, for private package access. Remote publication targets `https://maven.pkg.github.com/softiqcz/softiq-watchdog-core`. Build/check never publish. Consumer projects can use `-PwatchdogCoreDir=../softiq-watchdog-core` for local source substitution, or `-PuseMavenLocal` after local publication.

Tests are isolated and do not contact the database, payment provider, mail server or browser. Focused tasks remain available: `paymentRegressionTest`, `reportRegressionTest`, `portfolioRegressionTest`.

Keep entity/table mappings and API data shapes compatible with both apps. Services remain thin package-private facades exposed by `I...Service`; implementations belong in components. The existing `softiq-core-app:1.21.7` dependency is unchanged, including its transitive browser/test libraries.
