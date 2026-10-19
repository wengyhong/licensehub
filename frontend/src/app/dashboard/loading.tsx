export default function DashboardLoading() {
  return (
    <main
      aria-busy="true"
      aria-label="Loading dashboard"
      className="mx-auto max-w-6xl px-6 py-12"
    >
      <div className="animate-pulse">
        <div className="h-9 w-48 rounded bg-slate-200" />
        <div className="mt-3 h-5 w-80 max-w-full rounded bg-slate-200" />

        <div className="mt-8 rounded-lg border border-slate-200 bg-white p-6">
          <div className="h-6 w-36 rounded bg-slate-200" />
          <div className="mt-3 h-4 w-full rounded bg-slate-100" />
          <div className="mt-2 h-4 w-2/3 rounded bg-slate-100" />

          <div className="mt-6 h-10 w-36 rounded bg-slate-200" />
        </div>
      </div>
    </main>
  );
}