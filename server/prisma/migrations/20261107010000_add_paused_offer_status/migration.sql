-- A business can pause an offer and resume it later; while paused it is hidden from customers.
-- AlterEnum
ALTER TYPE "OfferStatus" ADD VALUE 'PAUSED';
