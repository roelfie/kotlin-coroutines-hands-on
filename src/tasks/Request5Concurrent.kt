package tasks

import contributors.*
import kotlinx.coroutines.*

suspend fun loadContributorsConcurrent(
    service: GitHubService,
    req: RequestData):
        List<User> = coroutineScope {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    var deferreds = repos.map { repo ->
        // Because we don't specify a CoroutineContext in the 'async' coroutine builder,
        // the dispatcher from the outer scope will be used (which is Dispatchers.Default in Contributors.kt).
        // This is considered best practice. It makes it, for instance, easier to test, because the caller can specify
        // the context (in case of a unit test, the TestCoroutineDispatcher).
        async {
            log("starting loading for ${repo.name}")
            delay(3000)
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }

    deferreds.awaitAll().flatten().aggregate()
}