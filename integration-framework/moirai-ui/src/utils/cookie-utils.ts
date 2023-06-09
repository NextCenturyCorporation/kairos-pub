import * as Cookies from "es-cookie";

const TOKEN_COOKIE_NAME = "authToken";
const TOKEN_COOKIE_ADMIN = "adminToken";

export interface TokenContainer {
  token: string;
}

export function setTokenInCookie(token: string, expirationTimer: number): void {
  let now = new Date().getTime();
  let expiration = new Date(now + expirationTimer + 10000);
  let tokenContainer: TokenContainer = {
    token: token
  };
  Cookies.set(TOKEN_COOKIE_NAME, JSON.stringify(tokenContainer), {
    path: "/",
    expires: expiration
  });
}

export function getToken(): string {
  let cookie: string = Cookies.get(TOKEN_COOKIE_NAME) || "";
  let tokenContainer: TokenContainer = cookie ? JSON.parse(cookie) : null;
  return tokenContainer ? tokenContainer.token : "";
}

export function hasToken(): boolean {
  let token = getToken();
  return token != null && token != "" ? true : false;
}

export function stacheAdminToken(token: string, expirationTimer: number): void {
  let now = new Date().getTime();
  let expiration = new Date(now + expirationTimer + 10000);
  let tokenContainer: TokenContainer = {
    token: token
  };
  Cookies.set(TOKEN_COOKIE_ADMIN, JSON.stringify(tokenContainer), {
    path: "/",
    expires: expiration
  });
}

export function getAdminToken(): string {
  let cookie: string = Cookies.get(TOKEN_COOKIE_ADMIN) || "";
  let tokenContainer: TokenContainer = cookie ? JSON.parse(cookie) : null;
  return tokenContainer ? tokenContainer.token : "";
}

export function clearAdminToken(): void {
  Cookies.remove(TOKEN_COOKIE_ADMIN, { path: "/" });
}

export function clearToken(): void {
  Cookies.remove(TOKEN_COOKIE_NAME, { path: "/" });
}
