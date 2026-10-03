package com.fairshare.ai;

import java.util.List;

public interface AiClient {
    AiDtos.Intent interpret(String message, List<String> memberNames);
}
