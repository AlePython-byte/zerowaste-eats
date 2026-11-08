-- CreateTable
CREATE TABLE "redemption_attempts" (
    "id" UUID NOT NULL,
    "reservationId" UUID NOT NULL,
    "attemptedAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "redemption_attempts_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "redemption_attempts_reservationId_attemptedAt_idx" ON "redemption_attempts"("reservationId", "attemptedAt");

-- AddForeignKey
ALTER TABLE "redemption_attempts" ADD CONSTRAINT "redemption_attempts_reservationId_fkey" FOREIGN KEY ("reservationId") REFERENCES "reservations"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- Like every other table: closed to the Data API roles, used only by the backend
ALTER TABLE "redemption_attempts" ENABLE ROW LEVEL SECURITY;
