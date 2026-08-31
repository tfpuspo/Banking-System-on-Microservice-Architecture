"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Landmark } from "lucide-react";
import { Card } from "@/components/Card";
import { Field } from "@/components/Field";
import { Button } from "@/components/Button";
import { graphqlRequest } from "@/lib/graphql";

const REGISTER_MUTATION = `
  mutation Register($email: String!, $password: String!, $fullName: String!) {
    register(email: $email, password: $password, fullName: $fullName) {
      userId
      email
    }
  }
`;

export default function RegisterPage() {
  const router = useRouter();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await graphqlRequest(REGISTER_MUTATION, { email, password, fullName });
      router.push("/login?registered=1");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-6 py-16">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex items-center justify-center gap-2">
          <Landmark className="text-brand-600" size={22} />
          <span className="text-lg font-semibold text-ink-900">Meridian</span>
        </div>

        <Card>
          <h1 className="mb-6 text-xl font-semibold text-ink-900">Open an account</h1>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Field
              label="Full name"
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
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
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            {error && (
              <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-bad">{error}</p>
            )}

            <Button type="submit" disabled={loading} className="mt-2 w-full">
              {loading ? "Opening…" : "Open account"}
            </Button>
          </form>
        </Card>

        <p className="mt-6 text-center text-sm text-ink-500">
          Already have an account?{" "}
          <Link href="/login" className="font-medium text-brand-600 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
