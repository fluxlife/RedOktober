CREATE TABLE images(
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   image_source TEXT NOT NULL,
   metadata jsonb NOT NULL
);

CREATE INDEX IDX_images_metadata
    ON images using GIN (metadata);