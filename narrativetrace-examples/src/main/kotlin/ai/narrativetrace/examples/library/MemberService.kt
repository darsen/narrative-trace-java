package ai.narrativetrace.examples.library

import ai.narrativetrace.core.annotation.NotTraced

interface MemberService {
    fun lookupMember(memberId: String, @NotTraced cardNumber: String): Member
}

class InMemoryMemberService : MemberService {

    private val members = mapOf(
        "M-001" to Member("M-001", "Alice", "4111-XXXX-XXXX-1234"),
        "M-002" to Member("M-002", "Bob", "5500-XXXX-XXXX-5678")
    )

    override fun lookupMember(memberId: String, cardNumber: String): Member =
        members[memberId] ?: throw IllegalArgumentException("Member not found: $memberId")
}
