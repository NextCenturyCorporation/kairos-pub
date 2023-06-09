import { Configuration, Middleware, RequestArgs } from "zeus-api";
import { getToken } from "@/utils/cookie-utils";

export interface JWTRequestArgs extends RequestArgs {
  withCredentials?: boolean;
}

export class JWTAuthInterceptor extends Configuration {
  private static config: JWTAuthInterceptor;

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
    return "/zeus";
  }

  public static get Instance(): Configuration {
    return JWTAuthInterceptor.config || (JWTAuthInterceptor.config = new this());
  }
}
