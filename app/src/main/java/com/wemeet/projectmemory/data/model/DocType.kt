package com.wemeet.projectmemory.data.model

/**
 * The set of Markdown documents a project can contain. Colors match the
 * diff-stripe / tab-dot palette from the UI mockup.
 */
enum class DocType(val id: String, val label: String, val fileName: String, val colorHex: String) {
    README("readme", "README", "README.md", "#8B95A5"),
    REQUIREMENTS("requirements", "requirements", "requirements.md", "#F2B84B"),
    TODO("todo", "todo", "todo.md", "#6FCF97"),
    ARCHITECTURE("architecture", "architecture", "architecture.md", "#9B8CFF"),
    IDEAS("ideas", "ideas", "ideas.md", "#FF8F6B"),
    DECISIONS("decisions", "decisions", "decisions.md", "#4FD1C5"),
    MEETING("meeting", "meeting", "meeting.md", "#63B3ED");

    companion object {
        fun fromId(id: String): DocType? = entries.find { it.id == id }
    }
}
