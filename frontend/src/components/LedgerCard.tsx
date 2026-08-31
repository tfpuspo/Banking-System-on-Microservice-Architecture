export function LedgerCard({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <div
      className={`rounded-sm border border-ink-line bg-ink-surface/60 bg-ruled-paper p-8 shadow-[0_1px_0_0_rgba(201,162,75,0.15)] ${className}`}
    >
      {children}
    </div>
  );
}
