package com.infusi.waflow.message;

public sealed interface SendResult {

    record Success(String messageId) implements SendResult {}

    record Failure(String error) implements SendResult {}
}
