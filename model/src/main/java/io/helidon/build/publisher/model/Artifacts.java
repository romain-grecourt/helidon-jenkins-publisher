package io.helidon.build.publisher.model;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Artifacts model.
 */
@JsonSerialize(using = JacksonSupport.ArtifactsSerializer.class)
public final class Artifacts {

    final List<Item> items;

    private Artifacts(List<Item> items) {
        this.items = items;
    }

    public List<Item> items() {
        return items;
    }

    /**
     * Find the files in the given directory.
     * @param dir directory path
     * @return Artifacts
     * @throws IOException if an IO error occurs
     */
    public static Artifacts find(Path dir) throws IOException {
        Objects.requireNonNull(dir, "dir is null");
        FileVisitorImpl visitor = new FileVisitorImpl(dir);
        Files.walkFileTree(dir, visitor);
        List<Item> items = visitor.dirItem != null ? visitor.dirItem.children : Collections.emptyList();
        return new Artifacts(items);
    }

    /**
     * Artifact item model.
     */
    public static abstract class Item {

        final String name;
        final String path;

        protected Item(String name, String path) {
            this.name = name;
            this.path =  path == null ? ("/" + name) : path;
        }

        @JsonProperty
        public String path() {
            return path;
        }

        @JsonProperty
        public String name() {
            return name;
        }
    }

    /**
     * Artifact directory item model.
     */
    public static final class DirItem extends Item {

        final List<Item> children;

        private DirItem(String name, String path, List<Item> children) {
            super(name, path);
            this.children = children;
        }

        @JsonProperty
        public List<Item> children() {
            return children;
        }
    }

    /**
     *
     * Artifact file item model.
     */
    public static final class FileItem extends Item {

        final String type;

        private FileItem(String name, String path, String type) {
            super(name, path);
            this.type = type;
        }

        @JsonProperty
        public String type() {
            return type;
        }
    }

    /**
     * File visitor.
     */
    private static class FileVisitorImpl implements FileVisitor<Path> {

        final Path root;
        final Deque<DirItem> stack;
        DirItem dirItem;

        FileVisitorImpl(Path dir) {
            root = dir;
            stack = new LinkedList<>();
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            stack.push(new DirItem(root.relativize(dir).toString(), dir.getFileName().toString(), new LinkedList<>()));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fname = file.getFileName().toString();
            int idx = fname.lastIndexOf(".");
            String type;
            if (idx > 0) {
                type = fname.substring(idx + 1, fname.length());
            } else {
                type = "";
            }
            stack.peek().children.add(new FileItem(fname, root.relativize(file).toString(), type));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            DirItem item = stack.pop();
            if (stack.isEmpty()) {
                dirItem = item;
            } else {
                stack.peek().children.add(item);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
