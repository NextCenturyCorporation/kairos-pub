import { Configuration, Middleware, RequestArgs } from "clotho-api";
import { getToken } from "@/utils/cookie-utils";

export interface JWTRequestArgs extends RequestArgs {
  withCredentials?: boolean;
}

export class JWTClothoAuthInterceptor extends Configuration {
  private static config: JWTClothoAuthInterceptor;

  private constructor() {
    const middleware: Middleware[] = [
      {
        pre(request: RequestArgs): JWTRequestArgs {
          return {
            ...request,
            withCredentials: true,
            headers: {
              ...request.headers,
              Authorization: "Bearer " + getToken()
            }
          };
        }
      }
    ];

    super({ middleware });
  }

  get basePath(): string {
    return "/clotho";
  }

  public static get Instance(): Configuration {
    return JWTClothoAuthInterceptor.config || (JWTClothoAuthInterceptor.config = new this());
  }
}
