package dev.genesshoan.fitnesstrackerapi.common.storage;

import java.util.Map;

public record UploadPolicy(String uploadUrl, Map<String, String> formFields) {}
