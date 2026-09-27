import { apiRequest } from "./apiClient.js";

export function getAccount(accessToken) {
  return apiRequest("GET", "/api/account", { accessToken });
}

/**
 * Called once right after login with values decoded from the ID token -
 * the access token used to authenticate this call doesn't reliably carry
 * email/name itself. See backend UserProvisioningService.updateProfile.
 */
export function updateProfile(accessToken, { email, displayName }) {
  return apiRequest("PUT", "/api/account/profile", { accessToken, body: { email, displayName } });
}

export function requestAccountDeletion(accessToken) {
  return apiRequest("DELETE", "/api/account", { accessToken });
}

export function cancelAccountDeletion(accessToken) {
  return apiRequest("POST", "/api/account/cancel-deletion", { accessToken });
}
