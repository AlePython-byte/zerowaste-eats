-- CreateTable
CREATE TABLE "ai_suggestion_usage" (
    "merchantProfileId" UUID NOT NULL,
    "day" DATE NOT NULL,
    "count" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMPTZ(3) NOT NULL,

    CONSTRAINT "ai_suggestion_usage_pkey" PRIMARY KEY ("merchantProfileId","day"),
    -- Released reservations lower the count; it must never go below zero
    CONSTRAINT "ai_suggestion_usage_count_check" CHECK ("count" >= 0)
);

-- AddForeignKey
ALTER TABLE "ai_suggestion_usage" ADD CONSTRAINT "ai_suggestion_usage_merchantProfileId_fkey" FOREIGN KEY ("merchantProfileId") REFERENCES "merchant_profiles"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- Like every other table: closed to the Data API roles, used only by the backend
ALTER TABLE "ai_suggestion_usage" ENABLE ROW LEVEL SECURITY;
