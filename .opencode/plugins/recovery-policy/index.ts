import { appendFile, mkdir } from "node:fs/promises"
import { join } from "node:path"
import { Plugin } from "@opencode/plugin"
import { decideRetry } from "./policy.mjs"

/**
 * Bounded transient retries for OpenCode V2.
 * Cross-provider task failover is intentionally coordinated at task boundaries;
 * changing model inside a failed write/tool turn could duplicate or corrupt work.
 */
export default Plugin.define({
  id: "project-recovery-policy",
  async setup(ctx) {
    const recoveryDir = join(ctx.location.directory, ".opencode", "recovery")
    const logPath = join(recoveryDir, "provider-errors.jsonl")

    const record = async (entry: Record<string, unknown>) => {
      try {
        await mkdir(recoveryDir, { recursive: true })
        await appendFile(logPath, `${JSON.stringify({ time: new Date().toISOString(), ...entry })}\n`, "utf8")
      } catch (error) {
        // Recovery logging must never break a model request. OpenCode's own logs remain available.
        console.warn("[project-recovery-policy] Could not write recovery log", error instanceof Error ? error.message : "unknown error")
      }
    }

    const registration = await ctx.session.hook("retry", async (event) => {
      const result = decideRetry(event)
      if (!result) return

      event.decision = result.decision
      await record({
        sessionID: event.sessionID,
        agent: event.agent,
        providerID: event.model.providerID,
        modelID: event.model.id,
        status: event.error.status ?? null,
        errorType: event.error.type,
        attempt: event.attempt,
        action: result.decision.retry ? "retry" : "stop",
        reason: result.reason,
      })
    })

    return async () => {
      await registration.dispose()
    }
  },
})
