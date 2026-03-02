export interface Liquor {
  id: number;
  name: string;
  type: string;
  category: string;
  abv?: number;
  age?: string;
  score?: number;
  price?: string;
  priceUsd?: number;
  priceKrw?: number;
  origin?: string;
  region?: string;
  volume?: string;
  volumeMl?: number;
  imageUrl?: string;
  about?: string;
  heritage?: string;
  profile?: LiquorProfile;
  tastingNotes?: string[];
  tastingDetail?: string;
  tastingDetailKo?: string;
  pairing?: string[];
  pairingKo?: string[];
  nameKo?: string;
  typeKo?: string;
  aboutKo?: string;
  heritageKo?: string;
  tastingNotesKo?: string[];
  suggestedImageKeyword?: string;
  dataSource?: string;
  status: string;
  createdAt: string;
  updatedAt?: string;
}

export interface LiquorProfile {
  [key: string]: number;
}

export interface CollectedSourceInfo {
  source: string;
  name?: string;
  fieldsFound: string[];
  highlights: Record<string, string | null>;
  originalTexts?: Record<string, string>;
}

export interface DisambiguationCandidate {
  name: string;
  nameKo?: string;
  description?: string;
  descriptionKo?: string;
  vintage?: number;
}

export interface AiSearchResult {
  name: string;
  type: string;
  category: string;
  abv?: number;
  age?: string;
  score?: number;
  price?: string;
  priceUsd?: number;
  priceKrw?: number;
  origin?: string;
  region?: string;
  volume?: string;
  volumeMl?: number;
  about?: string;
  heritage?: string;
  profile?: LiquorProfile;
  tastingNotes?: string[];
  tastingDetail?: string;
  tastingDetailKo?: string;
  pairing?: string[];
  pairingKo?: string[];
  bottleVisualDescription?: string;
  nameKo?: string;
  typeKo?: string;
  aboutKo?: string;
  heritageKo?: string;
  tastingNotesKo?: string[];
  suggestedImageKeyword?: string;
  imageUrl?: string;
  dataSource?: string;
  dataSources?: string[];
  collectedSources?: CollectedSourceInfo[];
  synthesisReasoning?: string;
}

export interface SuggestionResponse {
  found: false;
  message: string;
  messageKo?: string;
  suggestions: LiquorSuggestion[];
}

export interface LiquorSuggestion {
  name: string;
  nameKo?: string;
  reason: string;
  reasonKo?: string;
}

export type AiLookupResult = AiSearchResult | SuggestionResponse;

export function isSuggestionResponse(result: AiLookupResult): result is SuggestionResponse {
  return 'found' in result && result.found === false && 'suggestions' in result;
}
