package com.fs.exception;

public class ImportJobNotFoundException extends RuntimeException {

    public ImportJobNotFoundException(Long jobId) {
        super("Задача импорта не найдена: " + jobId);
    }
}
