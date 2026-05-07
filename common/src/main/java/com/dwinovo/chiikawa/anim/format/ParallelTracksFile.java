package com.dwinovo.chiikawa.anim.format;

import java.util.List;

/**
 * JSON DTO for the per-pet "parallel tracks" sidecar. Lives at
 * {@code assets/<ns>/parallel/<pet>.json}, named to match the model key.
 *
 * <h2>Schema</h2>
 * <pre>{@code
 * {
 *   "tracks": ["blink", "breath", "tail_idle"]
 * }
 * }</pre>
 *
 * <p>Each entry names an animation in the matching pet's animation file.
 * Animations listed here are looped continuously alongside the BASE / sub
 * slots and are sampled <em>after</em> them so they win on shared bones —
 * matching the YSM "post-parallel" semantic.
 *
 * <p>The schema is intentionally minimal. When per-track conditions
 * ("only wag tail when happy") become a concrete need, the parser can be
 * upgraded to accept polymorphic entries (string OR object with
 * {@code animation} + {@code condition}) without breaking existing files.
 */
public final class ParallelTracksFile {
    public List<String> tracks;
}
