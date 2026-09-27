import { apiRequest } from "./apiClient.js";

/** Returns { downloadUrl, expiresAt } - a time-limited presigned S3 URL, never a public/permanent one. */
export function getArtifactDownloadUrl(accessToken, artifactId) {
  return apiRequest("GET", `/api/artifacts/${artifactId}`, { accessToken });
}
