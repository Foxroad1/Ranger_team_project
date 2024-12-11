import qrcode
import json

def generate_qr_code(locations):
    qr_data = json.dumps(locations)
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4,
    )
    qr.add_data(qr_data)
    qr.make(fit=True)
    img = qr.make_image(fill='black', back_color='white')
    img.save('company_qr_code.png')

locations = {
    "locations": [
        {"latitude": 62.769, "longitude": 22.879, "name": "Home"},  # coordinates for Home
        {"latitude": 62.759, "longitude": 22.843, "name": "School"}    # Coordinates for School
    ],
    "company_id": "Bethon Worker"
}
generate_qr_code(locations)