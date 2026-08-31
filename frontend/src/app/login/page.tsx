"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Landmark } from "lucide-react";
import { Card } from "@/components/Card";
import { Field } from "@/components/Field";
import { Button } from "@/components/Button";
import { graphqlRequest } from "@/lib/graphql";
import { saveTokens } from "@/lib/auth";

const LOGIN_MUTATION = `
  mutation Login($email: String!, $password: String!) {
    login(email: $email, password: $password) {
      accessToken
      refreshToken
      expiresIn
    }
  }
`;

type LoginResponse = {
  login: { accessToken: string; refreshToken: string; expiresIn: number };
};

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const justRegistered = searchParams.get("registered") === "1";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const data = await graphqlRequest<LoginResponse>(LOGIN_MUTATION, { email, password });
      saveTokens(data.login.accessToken, data.login.refreshToken);
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="w-full max-w-sm">
      <div className="mb-8 flex items-center justify-center gap-2">
        <Landmark className="text-brand-600" size={22} />
        <span className="text-lg font-semibold text-ink-900">Meridian</span>
      </div>

      {justRegistered && (
        <p className="mb-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-good">
          Account opened. Sign in below.
        </p>
      )}

      <Card>
        <h1 className="mb-6 text-xl font-semibold text-ink-900">Sign in</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Field
            label="Email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Field
            label="Password"
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {error && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-bad">{error}</p>
          )}

          <Button type="submit" disabled={loading} className="mt-2 w-full">
            {loading ? "Signing in…" : "Sign in"}
          </Button>
        </form>
      </Card>

      <p className="mt-6 text-center text-sm text-ink-500">
        No account yet?{" "}
        <Link href="/register" className="font-medium text-brand-600 hover:underline">
          Open one
        </Link>
      </p>
    </div>
  );
}

export default function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center px-6 py-16">
      <Suspense fallback={null}>
        <LoginForm />
      </Suspense>
    </div>
  );
}
