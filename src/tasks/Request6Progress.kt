package tasks

import contributors.*

suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    var users = emptyList<User>()
    var ix = 0
    repos.forEach { repo ->
        val bodyList: List<User> = service
            .getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
        users = (users + bodyList).aggregate()
        ix++
        updateResults(users, ix == repos.size)
    }
}
