-- Extends image_type to support Guided 360deg Capture (Front/Back/Left/
-- Right/Top/Bottom), replacing the single generic 'SIDE' value used until
-- now. 'SIDE' is kept (not dropped) so any existing rows and the value
-- itself remain valid -- it now means "a side photo whose exact facing
-- wasn't captured via the guided flow", not a removed concept.
alter table images drop constraint images_image_type_check;
alter table images add constraint images_image_type_check
    check (image_type in ('FRONT', 'BACK', 'LEFT', 'RIGHT', 'TOP', 'BOTTOM', 'SIDE', 'OTHER'));
