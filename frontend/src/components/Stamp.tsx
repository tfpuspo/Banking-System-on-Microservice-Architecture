export function Stamp({ userId, roles }: { userId: string; roles: string[] }) {
  const shortId = userId.slice(0, 8);

  return (
    <div className="relative inline-flex -rotate-3 flex-col items-center justify-center border-[3px] border-brass px-8 py-6 text-center">
      <div className="absolute inset-[3px] border border-brass/40" />
      <span className="font-mono text-[10px] uppercase tracking-[0.3em] text-brass">
        Session Verified
      </span>
      <span className="mt-2 font-display text-2xl text-brass">{shortId}</span>
      <span className="mt-2 font-mono text-[10px] uppercase tracking-widest text-brass/70">
        {roles.join(" · ")}
      </span>
    </div>
  );
}
