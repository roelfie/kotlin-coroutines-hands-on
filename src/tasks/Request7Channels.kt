package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .bodyList()

        var allUsers = emptyList<User>()
        val userChannel = Channel<List<User>>(100) // buffered channel with max. 100 elements

        for (repo in repos) {
            launch {
                log("starting loading for ${repo.name}")
                userChannel.send(service.getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList())
            }
        }

        repeat(repos.size) {
            var users = userChannel.receive()
            allUsers = (allUsers + users).aggregate()
            updateResults(allUsers, it == repos.size - 1)
        }
    }
}
