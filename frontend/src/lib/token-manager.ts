export class TokenManager {
  private static instance: TokenManager | null = null
  private accessToken: string | null = null

  private constructor() {}

  public static getInstance(): TokenManager {
    if (!TokenManager.instance) {
      TokenManager.instance = new TokenManager()
    }
    return TokenManager.instance
  }

  public getAccessToken(): string | null {
    return this.accessToken
  }

  public setAccessToken(token: string | null): void {
    this.accessToken = token
  }

  public clearAccessToken(): void {
    this.accessToken = null
  }

  public hasAccessToken(): boolean {
    return Boolean(this.accessToken)
  }
}

export const tokenManager = TokenManager.getInstance()
