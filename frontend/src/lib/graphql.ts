// Talks to graphql-gateway through Kong. This runs in the BROWSER (all pages
// that use this are client components), so the URL must be reachable from
// the user's machine — localhost:8000 — not an internal Docker service name.
const API_URL = process.env.NEXT_PUBLIC_GRAPHQL_URL || "http://localhost:8000/graphql";

export class GraphQLError extends Error {}

export async function graphqlRequest<T>(
  query: string,
  variables?: Record<string, unknown>
): Promise<T> {
  const res = await fetch(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });

  const json = await res.json();

  if (json.errors?.length) {
    throw new GraphQLError(json.errors[0].message);
  }

  return json.data as T;
}
