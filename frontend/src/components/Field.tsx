import { InputHTMLAttributes } from "react";

export function Field({
  label,
  ...props
}: { label: string } & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-ink-700">{label}</span>
      <input
        {...props}
        className="w-full rounded-xl border border-ink-100 bg-white px-4 py-3 text-ink-900 outline-none transition-colors placeholder:text-ink-300 focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
      />
    </label>
  );
}
