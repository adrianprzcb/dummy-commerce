CREATE UNIQUE INDEX uq_product_image_position
    ON product_images(product_id, position);