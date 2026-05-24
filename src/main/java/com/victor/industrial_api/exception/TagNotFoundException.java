package com.victor.industrial_api.exception;

public class TagNotFoundException extends RuntimeException{
    public TagNotFoundException(String tagName) {
        super("Tag not found: " + tagName);
    }
}