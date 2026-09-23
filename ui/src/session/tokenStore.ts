const TOKEN_KEY = "dsa.accessToken";

export function readAccessToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function writeAccessToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken(): void {
  sessionStorage.removeItem(TOKEN_KEY);
}
