package io.helidon.jenkins.publisher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Flow run.
 */
final class FlowRun {

    private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();
    private final String jobName;
    private final String scmHead;
    private final String scmHash;
    private final long timestamp;
    private final String id;
    private final String desc;

    /**
     * Create a new flow run.
     * @param jobName the job name
     * @param scmHead the branch name that this run was triggered against
     * @param buildNumber the build number
     * @param scmHash the GIT commit id that this run was triggered against
     */
    FlowRun(String jobName, String scmHead, int buildNumber, long timestamp, String scmHash) {
        if (jobName == null || jobName.isEmpty()) {
            throw new IllegalArgumentException("job name is null or empty");
        }
        if (scmHead == null || scmHead.isEmpty()) {
            throw new IllegalArgumentException("scmHead is null or empty");
        }
        if (scmHash == null || scmHash.isEmpty()) {
            throw new IllegalArgumentException("scmHash is null or empty");
        }
        if (buildNumber <= 0) {
            throw new IllegalArgumentException("Invalid buid number");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("Invalid timestamp");
        }
        this.jobName = jobName;
        this.scmHead = scmHead;
        this.scmHash = scmHash;
        this.timestamp = timestamp;
        this.desc = jobName + "/" + scmHead + "/" + buildNumber + "/" + timestamp + "/" + scmHash;
        this.id = md5sum(desc.getBytes());
    }

    /**
     * Make a MD5 sum string out of the input data.
     * @param data data to checksum
     * @return String
     */
    private String md5sum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            byte[] digest = md.digest();
            StringBuilder r = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                r.append(HEX_CODE[(b >> 4) & 0xF]);
                r.append(HEX_CODE[(b & 0xF)]);
            }
            return r.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Get the ID for this run.
     * @return String
     */
    String id() {
        return id;
    }

    /**
     * Get the job name.
     * @return String
     */
    String jobName() {
        return jobName;
    }

    /**
     * Get the branch name for this run.
     * @return String
     */
    String scmHead() {
        return scmHead;
    }

    /**
     * Get the timestamp for this run.
     * @return long
     */
    long timestamp() {
        return timestamp;
    }

    /**
     * Get the GIT commit id for this run.
     * @return String
     */
    String scmHash() {
        return scmHash;
    }

    /**
     * Get the description for this run.
     * @return String
     */
    String desc() {
        return desc;
    }

    @Override
    public String toString() {
        return FlowRun.class.getSimpleName() + "{ "
                + " }";
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FlowRun other = (FlowRun) obj;
        return Objects.equals(this.id, other.id);
    }
}
