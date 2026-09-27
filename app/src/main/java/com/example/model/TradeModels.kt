package com.example.model

enum class TradeDirection {
    LONG, SHORT
}

enum class TradeVisibility {
    PUBLIC, PRIVATE
}

enum class TradeStatus {
    OPEN, WINNER, STOPPED
}

enum class TraderTier(val label: String) {
    FREE("FREE"),
    VERIFIED("VERIFIED"),
    PROP("PROP"),
    INSTITUTIONAL("INSTITUTIONAL"),
    LIVE("LIVE")
}

data class TradeComment(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorInitials: String,
    val isAuthorBadge: Boolean = false,
    val timeAgo: String,
    val content: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val authorReply: TradeComment? = null
)

data class Trade(
    val id: Long = 0,
    val pair: String,
    val direction: TradeDirection,
    val setupStrategy: String,
    val session: String = "London Session",
    val timeframe: String = "15M",
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val exitPrice: Double? = null,
    val positionSizeLots: Double = 1.0,
    val riskRewardRatio: String = "1:2.9",
    val rMultiple: Double = 2.0,
    val netGainDollars: Double = 0.0,
    val riskPercent: Double = 1.0,
    val maxRiskDollars: Double = 1000.0,
    val visibility: TradeVisibility = TradeVisibility.PUBLIC,
    val status: TradeStatus = TradeStatus.WINNER,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val authorName: String = "Alex Vance",
    val authorHandle: String = "@QuantAlex",
    val authorTier: TraderTier = TraderTier.VERIFIED,
    val isAuthorVerified: Boolean = true,
    val executionThesis: String = "",
    val psychologyNote: String? = null,
    val postMortemReflection: String? = null,
    val planAdherencePercent: Int = 100,
    val disciplineScore: Double = 5.0,
    val tags: List<String> = emptyList(),
    val chartDrawableRes: Int? = null,
    val chartImageUri: String? = null,
    val chartOverlayText: String? = null,
    val tapeSpeed: String? = null,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val commentsCount: Int = 0,
    val isUpvoted: Boolean = false,
    val isDownvoted: Boolean = false,
    val isBookmarked: Boolean = false,
    val brokerName: String = "IC Markets (cTrader Raw)",
    val slippagePips: Double = 0.1,
    val durationText: String = "2h 45m"
)

data class TraderProfile(
    val id: String,
    val name: String,
    val handle: String,
    val tier: TraderTier,
    val tierLabel: String,
    val winRate: String,
    val streak: String,
    val netRGain: String,
    val followers: String,
    val isVerified: Boolean = true,
    val isFollowing: Boolean = false
)

data class FxUser(
    val uid: String,
    val email: String,
    val name: String,
    val handle: String = "@anonymous",
    val tier: TraderTier = TraderTier.VERIFIED
)
