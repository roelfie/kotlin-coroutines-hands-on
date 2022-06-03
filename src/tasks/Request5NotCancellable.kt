package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

suspend fun loadContributorsNotCancellable(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    var deferreds = repos.map { repo ->
        // Because we don't specify a CoroutineContext, the dispatcher from the outer scope (Dispatchers.Default) will
        // be used.
        // Since we're not inside a coroutineScope, we have to specify one explicitly: the GlobalScope.
        GlobalScope.async {
            log("starting loading for ${repo.name}")
            delay(3000)
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }

    // Notice that in Request5Concurrent we did not need a 'return' statement because we were inside a coroutineScope.
    // Here we do...
    return deferreds.awaitAll().flatten().aggregate()
}