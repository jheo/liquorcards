export interface Liquor {
  id: number;
  name: string;
  type: string;
  category: string;
  abv?: number;
  age?: string;
  score?: number;
  price?: string;
  origin?: string;
  region?: string;
  volume?: string;
  imageUrl?: string;
  about?: string;
  heritage?: string;
  profile?: LiquorProfile;
  tastingNotes?: string[];
  nameKo?: string;
  typeKo?: string;
  aboutKo?: string;
  heritageKo?: string;
  tastingNotesKo?: string[];
  suggestedImageKeyword?: string;
  status: string;
  createdAt: string;
  updatedAt?: string;
}

export interface LiquorProfile {
  [key: string]: number;
}

export interface AiSearchResult {
  name: string;
  type: string;
  category: string;
  abv?: number;
  age?: string;
  score?: number;
  price?: string;
  origin?: string;
  region?: string;
  volume?: string;
  about?: string;
  heritage?: string;
  profile?: LiquorProfile;
  tastingNotes?: string[];
  nameKo?: string;
  typeKo?: string;
  aboutKo?: string;
  heritageKo?: string;
  tastingNotesKo?: string[];
  suggestedImageKeyword?: string;
  imageUrl?: string;
}
