package io.helidon.build.publisher.backend;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.PipelineInfo;
import io.helidon.build.publisher.model.InfoAugmenter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pipeline info augmenter to fill in the missing fields of {@link PipelineInfo}.
 */
final class GitHubInfoAugmenter implements InfoAugmenter {

    private static final Logger LOGGER = Logger.getLogger(GitHubInfoAugmenter.class.getName());
    private static final String GITHUB_URL = "https://github.com";
    private static final String GITHUB_GIT = "git@github.com";
    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String PULL_REF_PREFIX = "pull/";
    private static final int CONNECT_TIMEOUT = 30 * 1000; // 30s
    private static final int READ_TIMEOUT = 60 * 2 * 1000; // 2min
    private static final int TITLE_MAX_LENGTH = 100;

    private final GitHubClient ghClient;

    GitHubInfoAugmenter() {
        this.ghClient = new GitHubClient();
    }

    @Override
    public boolean process(PipelineInfo info) {
        String repoUrl = info.repositoryUrl();
        if (!(repoUrl.startsWith(GITHUB_URL)
                || repoUrl.startsWith(GITHUB_GIT))) {
            // supporting GitHub only
            return false;
        }
        if (repoUrl.endsWith(".git")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
        }
        if (repoUrl.startsWith(GITHUB_GIT)) {
            repoUrl = GITHUB_URL + "/" + repoUrl.substring(GITHUB_GIT.length() + 1);
        }
        info.repositoryUrl(repoUrl);
        String headRef = info.headRef();
        if (headRef != null) {
            try {
                String repo = repoUrl.substring(GITHUB_URL.length() + 1);
                if (headRef.startsWith(PULL_REF_PREFIX)) {
                    String pr = headRef.substring(PULL_REF_PREFIX.length());
                    GHPullRequestInfo prInfo = ghClient.pullRequestInfo(repo, pr);
                    if (prInfo != null) {
                        info.user(prInfo.user);
                        info.userUrl(GITHUB_URL + "/" + prInfo.user);
                        info.title("Pull Request #" + pr + ": " + prInfo.title);
                        info.headRef(prInfo.headRef);
                        String headRepoUrl = GITHUB_URL + "/" + prInfo.headRepo;
                        info.headRefUrl(headRepoUrl + "/" + prInfo.headRef);
                        info.commitUrl(headRepoUrl + "/commit/" + info.commit());
                        if (info.mergeCommit() != null) {
                            info.mergeCommitUrl(repoUrl + "/commit/" + info.mergeCommit());
                        }
                    }
                } else {
                    info.commitUrl(repoUrl + "/commit/" + info.commit());
                    info.headRefUrl(repoUrl + "/tree/" + headRef);
                    GHCommitInfo commitInfo = ghClient.commitInfo(repo, info.commit());
                    if (commitInfo != null) {
                        info.user(commitInfo.user);
                        info.userUrl(GITHUB_URL + "/" + commitInfo.user);
                        info.title(commitInfo.message);
                    }
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            } catch (Throwable ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return true;
    }

    private static String truncate(String message, int max) {
        String[] argsLines = message.trim().split("[\\r\\n]+");
        for (String argsLine : argsLines) {
            argsLine = argsLine.trim();
            if (argsLine.isEmpty()) {
                continue;
            }
            if (argsLine.length() > max) {
                return argsLine.substring(0, max) + " ...";
            }
            return argsLine;
        }
        return "";
    }

    /**
     * GitHub commit info.
     */
    private static final class GHCommitInfo {

        final String message;
        final String user;

        GHCommitInfo(String message, String user) {
            this.message = message;
            this.user = user;
        }

        @Override
        public String toString() {
            return GHCommitInfo.class.getSimpleName() + " {"
                    + " user=" + user
                    + ", message=" + message
                    + " }";
        }
    }

    /**
     * GitHub pull request info.
     */
    private static final class GHPullRequestInfo {

        final String title;
        final String user;
        final String headRef;
        final String headRepo;

        GHPullRequestInfo(String title, String user, String headRef, String headRepo) {
            this.title = title;
            this.user = user;
            this.headRef = headRef;
            this.headRepo = headRepo;
        }

        @Override
        public String toString() {
            return GHPullRequestInfo.class.getSimpleName() + " {"
                    + " user=" + user
                    + ", title=" + title
                    + ", headRef=" + headRef
                    + ", headRepo=" + headRepo
                    + " }";
        }
    }

    /**
     * Minimal GitHub API client based on HUC.
     */
    private static final class GitHubClient {

        final ObjectMapper mapper = new ObjectMapper();

        /**
         * Get the GitHub commit info for a commit.
         * @param repo repository
         * @param commit commit id
         * @return GHCommitInfo or {@code null} if not found
         * @throws IOException if an IO error occurs
         */
        GHCommitInfo commitInfo(String repo, String commit) throws IOException {
            JsonNode node = doRequest(GITHUB_API_URL + "/repos/" + repo + "/commits/" + commit);
            if (node != null) {
                String user = null;
                if (node.hasNonNull("author")) {
                    JsonNode authorNode = node.get("author");
                    if (authorNode.hasNonNull("login")) {
                        user = authorNode.get("login").asText(null);
                    }
                }
                String message = null;
                if (node.hasNonNull("commit")) {
                    JsonNode commitNode = node.get("commit");
                    if (commitNode.hasNonNull("message")) {
                        message = commitNode.get("message").asText(null);
                    }
                }
                if (message != null && user != null) {
                    return new GHCommitInfo(truncate(message, TITLE_MAX_LENGTH), user);
                }
            }
            return null;
        }

        /**
         * Get the GitHub pull request info for a pull request.
         * @param repo repository
         * @param pr commit id
         * @return GHCommitInfo or {@code null} if not found
         * @throws IOException if an IO error occurs
         */
        GHPullRequestInfo pullRequestInfo(String repo, String pr) throws IOException {
            JsonNode node = doRequest(GITHUB_API_URL + "/repos/" + repo + "/pulls/" + pr);
            if (node != null) {
                String title = null;
                if (node.hasNonNull("title")) {
                    title = node.get("title").asText(null);
                }
                String user = null;
                if (node.hasNonNull("user")) {
                    JsonNode authorNode = node.get("user");
                    if (authorNode.hasNonNull("login")) {
                        user = authorNode.get("login").asText(null);
                    }
                }
                String headRef = null;
                String headRepo = null;
                if (node.hasNonNull("head")) {
                    JsonNode headNode = node.get("head");
                    if (headNode.hasNonNull("ref")) {
                        headRef = headNode.get("ref").asText(null);
                    }
                    if (headNode.hasNonNull("repo")) {
                        JsonNode headRepoNode = headNode.get("repo");
                        if (headRepoNode.hasNonNull("full_name")) {
                            headRepo = headRepoNode.get("full_name").asText(null);
                        }
                    }
                }
                if (title != null && user != null && headRef != null && headRepo != null) {
                    return new GHPullRequestInfo(truncate(title, TITLE_MAX_LENGTH), user, headRef, headRepo);
                }
            }
            return null;
        }

        private JsonNode doRequest(String u) throws IOException {
            URL url = URI.create(u).toURL();

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "HTTP GET url={0}", url);
            }

            URLConnection con = url.openConnection();
            if (!(con instanceof HttpURLConnection)) {
                throw new IllegalStateException("Not an HttpURLConnection");
            }
            HttpURLConnection hcon = (HttpURLConnection) con;
            hcon.addRequestProperty("Accept", "application/vnd.github.v3+json");
            hcon.setRequestMethod("GET");
            hcon.setDoOutput(false);
            hcon.setDoInput(true);
            hcon.setConnectTimeout(CONNECT_TIMEOUT);
            hcon.setReadTimeout(READ_TIMEOUT);

            int code = hcon.getResponseCode();
            if (200 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code, url={1}, code={2}",
                        new Object[]{
                            url,
                            code
                        });
            } else {
                return mapper.readTree(hcon.getInputStream());
            }
            return null;
        }
    }
}
