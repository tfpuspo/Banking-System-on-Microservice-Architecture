export function Card({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`rounded-2xl border border-ink-100 bg-white p-6 shadow-card ${className}`}>
      {children}
    </div>
  );
}
