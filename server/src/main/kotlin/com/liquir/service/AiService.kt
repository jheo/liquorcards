package com.liquir.service

import com.liquir.dto.AiLookupResponse

interface AiService {
    fun lookupLiquor(name: String, provider: String = "claude"): AiLookupResponse
}
