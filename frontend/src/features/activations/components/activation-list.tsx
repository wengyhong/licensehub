import { DeactivateActivationButton } from "./deactivate-activation-button";
import type { MachineActivation } from "../types";

type ActivationListProps = {
  activations: MachineActivation[];
};

const dateFormatter = new Intl.DateTimeFormat("en-MY", {
  dateStyle: "medium",
  timeStyle: "short",
});

export function ActivationList({
  activations,
}: ActivationListProps) {
  return (
    <section className="mt-8">
      <h2 className="text-2xl font-semibold">Machine activations</h2>
      <p className="mt-1 text-sm text-slate-600">
        Machines that have used this license.
      </p>

      {activations.length === 0 ? (
        <div className="mt-5 rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
          <h3 className="font-medium">No machine activations</h3>
          <p className="mt-2 text-sm text-slate-600">
            Activations will appear when customer software uses this license.
          </p>
        </div>
      ) : (
        <div className="mt-5 space-y-4">
          {activations.map((activation) => (
            <article
              className="rounded-lg border border-slate-200 bg-white p-5"
              key={activation.id}
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <h3 className="font-medium">
                    {activation.machineName ?? "Unnamed machine"}
                  </h3>
                  <p className="mt-1 text-sm text-slate-500">
                    Activated{" "}
                    {dateFormatter.format(
                      new Date(activation.activatedAt),
                    )}
                  </p>
                  <p className="mt-1 text-sm text-slate-500">
                    Last seen{" "}
                    {dateFormatter.format(
                      new Date(activation.lastSeenAt),
                    )}
                  </p>
                </div>

                <span
                  className={
                    activation.active
                      ? "rounded-full bg-green-100 px-2.5 py-1 text-xs font-medium text-green-800"
                      : "rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700"
                  }
                >
                  {activation.active ? "ACTIVE" : "DEACTIVATED"}
                </span>
              </div>

              {activation.active && (
                <div className="mt-4">
                  <DeactivateActivationButton
                    activationId={activation.id}
                    licenseId={activation.licenseId}
                    machineName={activation.machineName}
                  />
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}