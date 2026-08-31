"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LayoutDashboard, LogOut, Landmark } from "lucide-react";
import { getRefreshToken, clearTokens } from "@/lib/auth";
import { graphqlRequest } from "@/lib/graphql";

const LOGOUT_MUTATION = `
  mutation Logout($refreshToken: String!) {
    logout(refreshToken: $refreshToken) { success }
  }
`;

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();

  async function handleLogout() {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        await graphqlRequest(LOGOUT_MUTATION, { refreshToken });
      } catch {
        // clear local tokens regardless
      }
    }
    clearTokens();
    router.push("/login");
  }

  return (
    <aside className="flex h-screen w-60 flex-shrink-0 flex-col border-r border-ink-100 bg-white px-4 py-6">
      <div className="mb-8 flex items-center gap-2 px-2">
        <Landmark className="text-brand-600" size={22} />
        <span className="text-lg font-semibold text-ink-900">Meridian</span>
      </div>

      <nav className="flex-1 space-y-1">
        <Link
          href="/dashboard"
          className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${
            pathname === "/dashboard"
              ? "bg-brand-50 text-brand-700"
              : "text-ink-500 hover:bg-ink-100 hover:text-ink-900"
          }`}
        >
          <LayoutDashboard size={18} />
          Dashboard
        </Link>
      </nav>

      <button
        onClick={handleLogout}
        className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-ink-500 transition-colors hover:bg-ink-100 hover:text-bad"
      >
        <LogOut size={18} />
        Sign out
      </button>
    </aside>
  );
}
